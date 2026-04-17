package com.filemanager.service;

import com.filemanager.entity.User;
import com.filemanager.repository.UserRepository;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuotaService {

    private final UserRepository userRepository;

    public QuotaService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void validateAndAddQuota(Long userId, Long fileSize) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WebApplicationException("Không tìm thấy người dùng", Response.Status.NOT_FOUND));

        // Kiểm tra dung lượng: Đã dùng + File chuẩn bị tải lên > Mức cho phép
        if (user.getUsedQuota() + fileSize > user.getMaxQuota()) {
            throw new WebApplicationException("Bạn đã hết dung lượng lưu trữ! Vui lòng nâng cấp.", Response.Status.BAD_REQUEST);
        }

        // Cập nhật lại dung lượng đã sử dụng
        user.setUsedQuota(user.getUsedQuota() + fileSize);
        userRepository.save(user);
    }
}
