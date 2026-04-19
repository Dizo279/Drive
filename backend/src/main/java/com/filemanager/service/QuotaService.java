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
    public void validateAndAddQuota(Long userId, long fileSizeToAdd) {
        // 1. Chỉ tìm đúng 1 dòng User trong Database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WebApplicationException("Không tìm thấy User", Response.Status.NOT_FOUND));

        // 2. Lấy dung lượng hiện tại và giới hạn
        long currentUsed = user.getUsedQuota() != null ? user.getUsedQuota() : 0L;
        long maxQuota = user.getMaxQuota() != null ? user.getMaxQuota() : 1073741824L;

        // 3. Kiểm tra xem file mới tải lên có làm tràn dung lượng không
        if (currentUsed + fileSizeToAdd > maxQuota) {
            throw new WebApplicationException("Đã vượt quá dung lượng lưu trữ cho phép (1GB)!", Response.Status.BAD_REQUEST);
        }

        // 4. Nếu hợp lệ -> Cập nhật trực tiếp vào User và Lưu lại
        user.setUsedQuota(currentUsed + fileSizeToAdd);
        userRepository.save(user);
    }
}
