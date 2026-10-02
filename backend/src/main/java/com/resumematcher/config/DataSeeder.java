package com.resumematcher.config;

import com.resumematcher.entity.RoleProfile;
import com.resumematcher.repository.RoleProfileRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the role_profiles table on first startup.
 *
 * CommandLineRunner: Spring calls run() once after the application context
 * is fully initialized.  We check if the table is empty (count == 0) so
 * that restarting the app doesn't insert duplicate rows.
 *
 * Keywords are chosen from typical fresher job postings by Indian IT
 * services companies (TCS, Infosys, Wipro, Cognizant, Accenture, Capgemini).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleProfileRepository roleProfileRepository;

    public DataSeeder(RoleProfileRepository roleProfileRepository) {
        this.roleProfileRepository = roleProfileRepository;
    }

    @Override
    public void run(String... args) {
        // Only seed if the table is empty — prevents duplicates on restart
        if (roleProfileRepository.count() > 0) {
            return;
        }

        roleProfileRepository.save(new RoleProfile(
                "Java Developer",
                "java, spring, springboot, hibernate, jdbc, maven, sql, rest, api, multithreading, collections, microservices",
                "Backend development using Java and the Spring ecosystem"
        ));

        roleProfileRepository.save(new RoleProfile(
                "Python Developer",
                "python, django, flask, pandas, numpy, sql, rest, api, automation, scripting, linux, git",
                "Backend or scripting roles using Python frameworks"
        ));

        roleProfileRepository.save(new RoleProfile(
                "Full Stack Developer",
                "html, css, javascript, react, angular, nodejs, spring, rest, sql, mongodb, bootstrap, git",
                "End-to-end web development covering frontend and backend"
        ));

        roleProfileRepository.save(new RoleProfile(
                "Data / ML Engineer",
                "python, pandas, numpy, tensorflow, sklearn, sql, statistics, matplotlib, jupyter, regression, classification, preprocessing",
                "Data pipelines, analytics, and machine-learning model development"
        ));

        roleProfileRepository.save(new RoleProfile(
                "Frontend Developer",
                "html, css, javascript, react, angular, vue, typescript, responsive, bootstrap, redux, webpack, dom",
                "Building user interfaces and client-side web applications"
        ));

        roleProfileRepository.save(new RoleProfile(
                "Testing / QA Engineer",
                "testing, selenium, automation, manual, testng, junit, api, jira, agile, regression, bug, quality",
                "Software quality assurance, manual and automated testing"
        ));

        roleProfileRepository.save(new RoleProfile(
                "Generic Software Engineer",
                "java, python, sql, git, agile, algorithms, structures, debugging, linux, networking, oops, sdlc",
                "General software engineering covering multiple technologies"
        ));

        System.out.println("DataSeeder: inserted 7 default role profiles.");
    }
}
