package com.sky.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class BaiduMapUtil {

    @Autowired
    private RestTemplate restTemplate;

    private static final String GEOCODING_URL = "https://api.map.baidu.com/geocoding/v3/";
    private static final String DIRECTION_URL = "https://api.map.baidu.com/directionlite/v1/driving";

    public double[] getLngLat(String address, String ak) {
        try {
            // 使用 POST 请求，避免 URL 长度限制
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("address", address);
            params.add("output", "json");
            params.add("ak", ak);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            log.info("请求地理编码，地址: {}", address);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(GEOCODING_URL, request, String.class);
            String response = responseEntity.getBody();
            log.info("百度地图地理编码响应: {}", response);

            JSONObject jsonObject = JSON.parseObject(response);
            if (jsonObject.getIntValue("status") != 0) {
                String errorMsg = jsonObject.getString("message");
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = jsonObject.getString("msg");
                }
                log.error("地理编码失败: status={}, error={}",
                        jsonObject.getIntValue("status"), errorMsg);
                return null;
            }

            JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
            double lng = location.getDoubleValue("lng");
            double lat = location.getDoubleValue("lat");

            return new double[]{lng, lat};

        } catch (Exception e) {
            log.error("获取经纬度失败", e);
            return null;
        }
    }

    public Integer getDistance(String origin, String destination, String ak) {
        try {
            // 距离计算接口参数较短，继续使用 GET
            String url = UriComponentsBuilder.fromHttpUrl(DIRECTION_URL)
                    .queryParam("origin", origin)
                    .queryParam("destination", destination)
                    .queryParam("ak", ak)
                    .toUriString();

            log.info("请求距离计算URL: {}", url);
            String response = restTemplate.getForObject(url, String.class);
            log.info("百度地图距离计算响应: {}", response);

            JSONObject jsonObject = JSON.parseObject(response);
            if (jsonObject.getIntValue("status") != 0) {
                String errorMsg = jsonObject.getString("message");
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = jsonObject.getString("msg");
                }
                log.error("距离计算失败: status={}, error={}",
                        jsonObject.getIntValue("status"), errorMsg);
                return null;
            }

            JSONObject result = jsonObject.getJSONObject("result");
            if (result == null || result.getJSONArray("routes") == null) {
                return null;
            }

            JSONObject route = result.getJSONArray("routes").getJSONObject(0);
            return route.getInteger("distance");

        } catch (Exception e) {
            log.error("计算距离失败", e);
            return null;
        }
    }

    /**
     * 判断是否在配送范围内（综合方法）
     * @param userAddress 用户地址
     * @param shopAddress 商家地址
     * @param ak 百度地图AK
     * @param maxDistance 最大配送距离（米）
     * @return true-在范围内，false-超出范围
     */
    public boolean isInDeliveryRange(String userAddress, String shopAddress,
                                     String ak, int maxDistance) {
        try {
            // 第1步：获取用户地址的经纬度
            double[] userLngLat = getLngLat(userAddress, ak);
            if (userLngLat == null) {
                log.warn("无法获取用户地址经纬度: {}", userAddress);
                return false;
            }

            // 第2步：获取商家地址的经纬度
            double[] shopLngLat = getLngLat(shopAddress, ak);
            if (shopLngLat == null) {
                log.warn("无法获取商家地址经纬度: {}", shopAddress);
                return false;
            }

            // 第3步：计算距离
            // 注意：百度地图要求格式是 "纬度,经度"
            String origin = userLngLat[1] + "," + userLngLat[0];
            String destination = shopLngLat[1] + "," + shopLngLat[0];
            Integer distance = getDistance(origin, destination, ak);

            if (distance == null) {
                log.warn("无法计算距离");
                return false;
            }

            // 第4步：判断是否超过最大距离
            log.info("配送距离: {}米，最大配送距离: {}米", distance, maxDistance);
            return distance <= maxDistance;

        } catch (Exception e) {
            log.error("判断配送范围失败", e);
            return false;
        }
    }
}
