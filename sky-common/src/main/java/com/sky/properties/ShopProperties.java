package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component                    // ① 把这个类交给Spring管理
@ConfigurationProperties(prefix = "sky.shop")  // ② 自动读取配置文件
@Data                         // ③ 自动生成getter/setter
public class ShopProperties {

    private String address;           // 对应配置文件中的 sky.shop.address
    private String baiduMapAk;        // 对应配置文件中的 sky.shop.baidu-map-ak
}

