package com.restosuite.repo;

import com.restosuite.domain.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AppSettingRepository extends JpaRepository<AppSetting, Long>, JpaSpecificationExecutor<AppSetting> {

    java.util.Optional<AppSetting> findByRestaurantIdAndSettingKey(Long restaurantId, String settingKey);
    java.util.List<AppSetting> findByRestaurantIdOrderBySettingKeyAsc(Long restaurantId);


}
