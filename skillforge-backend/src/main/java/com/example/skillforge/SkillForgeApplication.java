// Author: Gowtham B
// SkillForge – AI-Driven Adaptive Learning and Exam Generator
// Individual Portfolio Project

package com.example.skillforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SkillForgeApplication {

	public static void main(String[] args) {

        SpringApplication.run(SkillForgeApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("✅ SkillForge Backend Started Successfully!");
        System.out.println("🚀 Server running on: http://localhost:8080");
        System.out.println("📚 API Docs: http://localhost:8080/api/health");
        System.out.println("========================================\n");
        
	}

}
