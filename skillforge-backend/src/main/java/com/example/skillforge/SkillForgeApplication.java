// Author: Gowtham B
// SkillForge – AI-Driven Adaptive Learning and Exam Generator
// Individual Portfolio Project

package com.example.skillforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class SkillForgeApplication {

	public static void main(String[] args) {
		// Load environment variables from .env file
		try {
			Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
			dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
		} catch (Exception e) {
			System.out.println("⚠️  Warning: Could not load .env file. Using system environment variables if available.");
		}

        SpringApplication.run(SkillForgeApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("✅ SkillForge Backend Started Successfully!");
        System.out.println("🚀 Server running on: http://localhost:8080");
        System.out.println("📚 API Docs: http://localhost:8080/api/health");
        System.out.println("========================================\n");
        
	}

}
