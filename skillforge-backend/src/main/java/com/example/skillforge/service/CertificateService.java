

package com.example.skillforge.service;

import com.example.skillforge.model.entity.*;
import com.example.skillforge.repository.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
public class CertificateService {

        @Autowired
        private CertificateRepository certificateRepository;

        @Autowired
        private CourseRepository courseRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private CompletionService completionService;

        @Autowired
        private StudentRepository studentRepository;

        @Autowired
        private EnrollmentRepository enrollmentRepository;

        @Autowired
        private ProgressRepository progressRepository;

        @org.springframework.beans.factory.annotation.Value("${skillforge.verification.base-url:http://localhost:5173/verify/}")
        private String verificationBaseUrl;

        @Transactional
        public Certificate generateCertificate(Long studentId, Long courseId) {
                // 1. Resolve User/Student 
                // The parameter passed is the Student ID (PK)
                Student studentEntity = studentRepository.findById(studentId)
                        .orElseThrow(() -> new com.example.skillforge.exception.CertificateGenerationException("Student not found"));
                User user = studentEntity.getUser();

                // 2. Get Enrollment record
                Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentEntity.getId(), courseId)
                        .orElseThrow(() -> new com.example.skillforge.exception.CertificateGenerationException("You are not enrolled in this course"));

                // 3. Check Progress record as the source of truth for completion
                java.util.Optional<com.example.skillforge.model.entity.Progress> progress = 
                    progressRepository.findByStudentIdAndCourseId(studentEntity.getId(), courseId);
                
                Integer progressPercentage = progress.map(p -> p.getCompletionPercentage()).orElse(enrollment.getCompletionPercentage());

                // 4. If progress shows 100% or enrollment is marked complete, allow certificate generation
                boolean isCompleteViaProgress = progressPercentage != null && progressPercentage >= 100;
                boolean isCompleteViaEnrollment = Boolean.TRUE.equals(enrollment.getIsCompleted());

                if (!isCompleteViaProgress && !isCompleteViaEnrollment) {
                        throw new com.example.skillforge.exception.CertificateGenerationException(
                                String.format("Course is not completed yet. Current progress: %d%%", progressPercentage != null ? progressPercentage : 0)
                        );
                }

                // 5. Sync Enrollment record if Progress shows completion but Enrollment doesn't
                if (isCompleteViaProgress && !isCompleteViaEnrollment) {
                        enrollment.setCompletionPercentage(progressPercentage != null ? progressPercentage : 100);
                        enrollment.setIsCompleted(true);
                        enrollment.setCompletedAt(java.time.LocalDateTime.now());
                        enrollmentRepository.save(enrollment);
                }

                // 6. Check idempotency using the USER ID (since Certificate links to User)
                java.util.Optional<Certificate> existingCert = certificateRepository.findByStudentIdAndCourseId(user.getId(),
                                courseId);
                if (existingCert.isPresent())
                        return existingCert.get();

                // 7. Get and validate Course
                Course course = courseRepository.findById(courseId)
                                .orElseThrow(() -> new com.example.skillforge.exception.CertificateGenerationException("Course not found"));

                // 8. Create and save Certificate
                Certificate certificate = Certificate.builder()
                                .student(user)
                                .course(course)
                                .uid(UUID.randomUUID().toString())
                                .studentNameSnapshot(user.getName())
                                .courseNameSnapshot(course.getTitle())
                                .build();

                return certificateRepository.save(certificate);
        }

        @Transactional
        public byte[] generateCertificatePdf(String uid) throws Exception {

                Certificate cert = certificateRepository.findByUid(uid)
                                .orElseThrow(() -> new RuntimeException("Certificate not found"));

                Document document = new Document(PageSize.A4.rotate());
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                PdfWriter writer = PdfWriter.getInstance(document, out);
                document.open();

                PdfContentByte canvas = writer.getDirectContent();

                // ===== BORDER =====
                drawElegantBorder(canvas, PageSize.A4.rotate());

                // ===== COLORS =====
                BaseColor blue = new BaseColor(0, 124, 195);
                BaseColor darkBlue = new BaseColor(0, 80, 130);
                BaseColor darkText = new BaseColor(60, 60, 60);
                BaseColor red = new BaseColor(220, 50, 50);

                // ===== FONTS =====
                Font header = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 38, blue);
                Font title = FontFactory.getFont(FontFactory.HELVETICA, 24, blue);
                Font name = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 42, darkBlue);
                Font text = FontFactory.getFont(FontFactory.HELVETICA, 16, darkText);
                Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, darkText);
                Font congrats = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 14, red);
                Font small = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.GRAY);
                Font signFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 18, blue);

                float cx = 421;

                ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, new Phrase("SkillForge", header), cx, 500, 0);
                ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                                new Phrase("COURSE COMPLETION CERTIFICATE", title), cx, 440, 0);
                ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                                new Phrase("The certificate is awarded to", text), cx, 390, 0);
                ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                                new Phrase(cert.getStudent().getName(), name), cx, 330, 0);
                ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                                new Phrase("for successfully completing the course", text), cx, 280, 0);
                ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                                new Phrase(cert.getCourseNameSnapshot(), bold), cx, 250, 0);

                String dateStr = cert.getIssuedAt().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
                ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                                new Phrase("on " + dateStr, text), cx, 220, 0);

                ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                                new Phrase("SkillForge Learning Platform",
                                                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, blue)),
                                cx, 180, 0);

                ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                                new Phrase("Congratulations! You make us proud!", congrats), cx, 150, 0);

                // ===== QR =====
                Image qr = generateQrCodeImage(verificationBaseUrl + cert.getUid());
                qr.scaleAbsolute(80, 80);
                qr.setAbsolutePosition(70, 70);
                document.add(qr);

                ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                                new Phrase("Issued: " + dateStr, small), 70, 60, 0);
                ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                                new Phrase("ID: " + cert.getUid(), small), 70, 50, 0);

                // ===== SIGNATURE =====
                // float sigX = 732;
                // ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                // new Phrase("Gowtham B", signFont), sigX, 150, 0);
                // ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                // new Phrase("Authorized Signatory", small), sigX, 115, 0);
                // ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                // new Phrase("SkillForge Learning Platform", small), sigX, 50, 0);
                // ===== SIGNATURE IMAGE =====
                // Image signImg = Image.getInstance(
                // getClass().getResource("/signature/gowtham-sign.png"));
                // signImg.scaleAbsolute(120, 45); // width x height
                // signImg.setAbsolutePosition(
                // PageSize.A4.rotate().getWidth() - 190, 150); // right-aligned
                // document.add(signImg);

                // ===== SIGNATURE (FINAL FINE-TUNED POSITION) =====

                // // Same QR inset
                // float rightInset = 70;
                // float pageWidth = PageSize.A4.rotate().getWidth();

                // // ⬅️ Move LEFT by increasing block width effect
                // float sigCenterX = pageWidth - (rightInset + 65); // was 40 → now 45

                // // ⬇️ Move DOWN slightly
                // float sigBaseY = 110; // was 130 → now 120

                // // Signature name (italic, professional)
                // Font signNameFont = FontFactory.getFont(
                // FontFactory.HELVETICA_OBLIQUE, 16, darkBlue);

                // ColumnText.showTextAligned(
                // canvas,
                // Element.ALIGN_CENTER,
                // new Phrase("Gowtham B", signNameFont),
                // sigCenterX,
                // sigBaseY,
                // 0
                // );

                // // Designation
                // ColumnText.showTextAligned(
                // canvas,
                // Element.ALIGN_CENTER,
                // new Phrase("Authorized Signatory", small),
                // sigCenterX,
                // sigBaseY - 18,
                // 0
                // );

                // // Organization
                // ColumnText.showTextAligned(
                // canvas,
                // Element.ALIGN_CENTER,
                // new Phrase("SkillForge Learning Platform", small),
                // sigCenterX,
                // sigBaseY - 34,
                // 0
                // );
                float rightInset = 70;
                float pageWidth = PageSize.A4.rotate().getWidth();

                // Center of signature block (mirror of QR)
                float sigCenterX = pageWidth - (rightInset + 60);

                // Base line for NAME
                float sigBaseY = 100;

                Image signImg = Image.getInstance(
                                getClass().getResource("/signature/gowtham-sign.png"));

                float imgWidth = 100;
                float imgHeight = 35;

                signImg.scaleAbsolute(imgWidth, imgHeight);

                // ⬆️ Image ABOVE name (important)
                float imgX = sigCenterX - (imgWidth / 2);
                float imgY = sigBaseY + 15;
                // float imgY = 145;

                signImg.setAbsolutePosition(imgX, imgY);
                document.add(signImg);

                Font signNameFont = FontFactory.getFont(
                                FontFactory.HELVETICA_OBLIQUE, 16, darkBlue);

                // Name
                ColumnText.showTextAligned(
                                canvas,
                                Element.ALIGN_CENTER,
                                new Phrase("Gowtham B", signNameFont),
                                sigCenterX,
                                sigBaseY,
                                0);

                // Designation
                ColumnText.showTextAligned(
                                canvas,
                                Element.ALIGN_CENTER,
                                new Phrase("Authorized Signatory", small),
                                sigCenterX,
                                sigBaseY - 18,
                                0);

                // Organization
                ColumnText.showTextAligned(
                                canvas,
                                Element.ALIGN_CENTER,
                                new Phrase("SkillForge Learning Platform", small),
                                sigCenterX,
                                sigBaseY - 34,
                                0);

                document.close();
                return out.toByteArray();
        }

        // ================= BORDER METHODS =================

        private void drawElegantBorder(PdfContentByte canvas, Rectangle page) {

                float x = 20, y = 20;
                float w = page.getWidth() - 40;
                float h = page.getHeight() - 40;

                // float rOuter = 12;
                // float gap = 7;
                // float rInner = 5;
                float rOuter = 12;
                float gap = 6; // increase gap slightly
                float rInner = 10; // inner curve almost same as outer

                BaseColor color = new BaseColor(60, 75, 90);
                canvas.setColorStroke(color);
                canvas.setColorFill(color);

                // Outer (Concave)
                canvas.setLineWidth(2f);
                drawInvertedRoundedRect(canvas, x, y, w, h, rOuter);
                canvas.closePath();
                canvas.stroke();

                // Inner (Convex)
                canvas.setLineWidth(1.0f);
                drawInvertedRoundedRect(canvas, x + gap, y + gap, w - 2 * gap, h - 2 * gap, rInner);
                canvas.closePath();
                canvas.stroke();

                // Corner dots
                float d = 3, r = 1.8f;
                canvas.circle(x + d, y + d, r);
                canvas.circle(x + w - d, y + d, r);
                canvas.circle(x + d, y + h - d, r);
                canvas.circle(x + w - d, y + h - d, r);
                canvas.fill();
        }

        private void drawInvertedRoundedRect(PdfContentByte c, float x, float y, float w, float h, float r) {
                float k = 0.552284749831f, kr = k * r;

                c.moveTo(x + r, y);
                c.lineTo(x + w - r, y);
                c.curveTo(x + w - r, y + kr, x + w - kr, y + r, x + w, y + r);
                c.lineTo(x + w, y + h - r);
                c.curveTo(x + w - kr, y + h - r, x + w - r, y + h - kr, x + w - r, y + h);
                c.lineTo(x + r, y + h);
                c.curveTo(x + r, y + h - kr, x + kr, y + h - r, x, y + h - r);
                c.lineTo(x, y + r);
                c.curveTo(x + kr, y + r, x + r, y + kr, x + r, y);
        }

        private void drawRoundedRect(PdfContentByte c, float x, float y, float w, float h, float r) {
                float k = 0.552284749831f, kr = k * r;

                c.moveTo(x + r, y);
                c.lineTo(x + w - r, y);
                c.curveTo(x + w - r + kr, y, x + w, y + r - kr, x + w, y + r);
                c.lineTo(x + w, y + h - r);
                c.curveTo(x + w, y + h - r + kr, x + w - r + kr, y + h, x + w - r, y + h);
                c.lineTo(x + r, y + h);
                c.curveTo(x + r - kr, y + h, x, y + h - r + kr, x, y + h - r);
                c.lineTo(x, y + r);
                c.curveTo(x, y + r - kr, x + r - kr, y, x + r, y);
        }

        private Image generateQrCodeImage(String text) throws Exception {
                QRCodeWriter writer = new QRCodeWriter();
                BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 150, 150);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(matrix, "PNG", out);
                return Image.getInstance(out.toByteArray());
        }

        public Certificate getCertificateByUid(String uid) {
                return certificateRepository.findByUid(uid)
                                .orElseThrow(() -> new RuntimeException("Certificate not found"));
        }
}
