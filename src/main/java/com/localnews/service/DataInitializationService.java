package com.localnews.service;

import com.localnews.entity.AdminUser;
import com.localnews.entity.District;
import com.localnews.repository.AdminUserRepository;
import com.localnews.repository.DistrictRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class DataInitializationService implements CommandLineRunner {

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) throws Exception {
        initializeDistricts();
        initializeAdminUser();
    }

    private void initializeDistricts() {
        if (districtRepository.count() == 0) {
            List<String[]> districts = Arrays.asList(
                new String[]{"Agra", "AGR"},
                new String[]{"Aligarh", "ALG"},
                new String[]{"Allahabad", "ALD"},
                new String[]{"Bareilly", "BRL"},
                new String[]{"Ghaziabad", "GZB"},
                new String[]{"Gorakhpur", "GKP"},
                new String[]{"Kanpur", "KNP"},
                new String[]{"Lucknow", "LKW"},
                new String[]{"Meerut", "MRT"},
                new String[]{"Moradabad", "MBD"},
                new String[]{"Varanasi", "VNS"}
            );

            for (String[] districtData : districts) {
                District district = new District(districtData[0], districtData[1]);
                districtRepository.save(district);
            }

            System.out.println("✅ Districts initialized: " + districts.size() + " districts added");
        }
    }

    private void initializeAdminUser() {
        if (adminUserRepository.count() == 0) {
            AdminUser admin = new AdminUser();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@localnews.com");

            adminUserRepository.save(admin);

            System.out.println("✅ Default admin user created - Username: admin, Password: admin123");
        }
    }
}
