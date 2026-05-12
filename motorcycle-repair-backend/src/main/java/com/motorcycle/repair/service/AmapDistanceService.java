package com.motorcycle.repair.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.motorcycle.repair.config.AmapConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class AmapDistanceService {

    private static final Logger log = LoggerFactory.getLogger(AmapDistanceService.class);

    @Autowired
    private AmapConfig amapConfig;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public double calculateDistance(String originAddress, String destAddress) {
        try {
            String originCity = extractCity(originAddress);
            String destCity = extractCity(destAddress);
            String originGeo = geocode(originAddress, originCity);
            String destGeo = geocode(destAddress, destCity);
            if (originGeo == null || destGeo == null) {
                log.warn("地理编码失败: origin={} city={} result={}, dest={} city={} result={}",
                        originAddress, originCity, originGeo, destAddress, destCity, destGeo);
                return -1;
            }
            return getDrivingDistance(originGeo, destGeo);
        } catch (Exception e) {
            log.error("计算距离异常: {}", e.getMessage(), e);
            return -1;
        }
    }

    public double calculateDistanceByCoords(double originLng, double originLat, String destAddress, String coordType) {
        try {
            double[] coords;
            if ("gcj02".equalsIgnoreCase(coordType)) {
                coords = new double[]{originLat, originLng};
            } else {
                coords = wgs84ToGcj02(originLat, originLng);
            }
            String destCity = extractCity(destAddress);
            String destGeo = geocode(destAddress, destCity);
            if (destGeo == null) {
                log.warn("目标地址地理编码失败: dest={}", destAddress);
                return -1;
            }
            String originGeo = String.format("%.6f,%.6f", coords[1], coords[0]);
            log.info("坐标类型={}, originGeo={}, destGeo={}", coordType, originGeo, destGeo);
            return getDrivingDistance(originGeo, destGeo);
        } catch (Exception e) {
            log.error("坐标计算距离异常: {}", e.getMessage(), e);
            return -1;
        }
    }

    public String reverseGeocode(double lng, double lat, String coordType) {
        try {
            double[] gcj;
            if ("gcj02".equalsIgnoreCase(coordType)) {
                gcj = new double[]{lat, lng};
            } else {
                gcj = wgs84ToGcj02(lat, lng);
            }
            String urlStr = amapConfig.getBaseUrl() + "/geocode/regeo?key=" + amapConfig.getApiKey()
                    + "&location=" + String.format("%.6f", gcj[1]) + "," + String.format("%.6f", gcj[0]);
            log.info("高德逆地理编码请求(coordType={}): lng={}, lat={}", coordType, gcj[1], gcj[0]);
            java.net.URI uri = new java.net.URI(urlStr);
            String response = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(response);
            String status = root.path("status").asText();
            if ("1".equals(status)) {
                String formattedAddress = root.path("regeocode").path("formatted_address").asText();
                log.info("逆地理编码成功: {},{} -> {}", gcj[1], gcj[0], formattedAddress);
                return formattedAddress;
            } else {
                log.warn("逆地理编码失败: status={}, info={}", status, root.path("info").asText());
            }
            return null;
        } catch (Exception e) {
            log.error("逆地理编码异常: {}", e.getMessage(), e);
            return null;
        }
    }

    private static final double PI = 3.1415926535897932384626;
    private static final double A = 6378245.0;
    private static final double EE = 0.00669342162296594323;

    private double[] wgs84ToGcj02(double lat, double lng) {
        if (outOfChina(lat, lng)) {
            return new double[]{lat, lng};
        }
        double dLat = transformLat(lng - 105.0, lat - 35.0);
        double dLng = transformLng(lng - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
        return new double[]{lat + dLat, lng + dLng};
    }

    private boolean outOfChina(double lat, double lng) {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
    }

    private double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private double transformLng(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }

    private String extractCity(String address) {
        if (address == null) return null;
        for (String city : new String[]{"北京市", "上海市", "天津市", "重庆市"}) {
            if (address.contains(city)) return city.replace("市", "");
        }
        String[] provinces = {"河北", "山东", "河南", "山西", "辽宁", "吉林", "黑龙江",
                "江苏", "浙江", "安徽", "福建", "江西", "湖北", "湖南", "广东", "海南", "四川",
                "贵州", "云南", "陕西", "甘肃", "青海", "台湾", "内蒙古", "广西", "西藏", "宁夏", "新疆"};
        for (String province : provinces) {
            int idx = address.indexOf(province);
            if (idx >= 0) {
                String afterProvince = address.substring(idx + province.length());
                if (afterProvince.startsWith("省")) afterProvince = afterProvince.substring(1);
                else if (afterProvince.startsWith("自治区")) afterProvince = afterProvince.substring(3);
                for (String suffix : new String[]{"市", "自治州", "地区", "盟"}) {
                    int si = afterProvince.indexOf(suffix);
                    if (si > 0) {
                        return afterProvince.substring(0, si + suffix.length());
                    }
                }
            }
        }
        return null;
    }

    public String geocode(String address, String city) {
        try {
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(amapConfig.getBaseUrl()).append("/geocode/geo?key=").append(amapConfig.getApiKey())
                    .append("&address=").append(URLEncoder.encode(address, StandardCharsets.UTF_8));
            if (city != null && !city.isEmpty()) {
                urlBuilder.append("&city=").append(URLEncoder.encode(city, StandardCharsets.UTF_8));
            }
            String urlStr = urlBuilder.toString();
            log.info("高德地理编码请求: address={}, city={}", address, city);
            java.net.URI uri = new java.net.URI(urlStr);
            String response = restTemplate.getForObject(uri, String.class);
            log.info("高德地理编码响应: {}", response != null && response.length() > 500 ? response.substring(0, 500) : response);
            JsonNode root = objectMapper.readTree(response);
            String status = root.path("status").asText();
            String infoCode = root.path("infocode").asText();
            String info = root.path("info").asText();
            if ("1".equals(status)) {
                JsonNode geocodes = root.path("geocodes");
                if (geocodes.isArray() && geocodes.size() > 0) {
                    String location = geocodes.get(0).path("location").asText();
                    log.info("地理编码成功: address={} -> location={}", address, location);
                    return location;
                }
                log.warn("地理编码返回成功但无结果: address={}", address);
            } else {
                log.warn("高德API返回错误: status={}, infocode={}, info={}", status, infoCode, info);
            }
            return null;
        } catch (Exception e) {
            log.error("地理编码异常: {}", e.getMessage(), e);
            return null;
        }
    }

    private double getDrivingDistance(String origin, String destination) {
        try {
            String urlStr = amapConfig.getBaseUrl() + "/direction/driving?key=" + amapConfig.getApiKey()
                    + "&origin=" + origin + "&destination=" + destination;
            log.info("高德驾车距离请求: origin={}, destination={}", origin, destination);
            java.net.URI uri = new java.net.URI(urlStr);
            String response = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(response);
            String status = root.path("status").asText();
            if ("1".equals(status)) {
                JsonNode route = root.path("route");
                JsonNode paths = route.path("paths");
                if (paths.isArray() && paths.size() > 0) {
                    double distanceMeters = paths.get(0).path("distance").asDouble();
                    log.info("高德驾车距离: {}米", distanceMeters);
                    return distanceMeters / 1000.0;
                }
            } else {
                log.warn("高德驾车API返回错误: status={}, infocode={}, info={}",
                        status, root.path("infocode").asText(), root.path("info").asText());
            }
            return -1;
        } catch (Exception e) {
            log.error("驾车距离计算异常: {}", e.getMessage(), e);
            return -1;
        }
    }

    public String verifyApiKey() {
        try {
            String url = amapConfig.getBaseUrl() + "/geocode/geo?key=" + amapConfig.getApiKey() + "&address=" + URLEncoder.encode("北京市朝阳区", StandardCharsets.UTF_8);
            log.info("verifyApiKey请求URL: {}", url);
            java.net.URI uri = new java.net.URI(url);
            String response = restTemplate.getForObject(uri, String.class);
            log.info("verifyApiKey响应: {}", response != null && response.length() > 300 ? response.substring(0, 300) : response);
            JsonNode root = objectMapper.readTree(response);
            String status = root.path("status").asText();
            String info = root.path("info").asText();
            String infoCode = root.path("infocode").asText();
            if ("1".equals(status)) {
                return "API Key有效，高德地图服务正常";
            } else {
                String hint = switch (infoCode) {
                    case "10001" -> "Key无效或过期，请检查是否为高德开放平台Web服务类型的Key";
                    case "10010" -> "Key类型不匹配，当前Key可能是Web端JS API Key，请改用Web服务Key";
                    case "10002" -> "Key没有权限调用此服务";
                    case "10003" -> "Key日调用量超限";
                    case "10004" -> "Key调用频率超限";
                    default -> "错误码:" + infoCode + " 信息:" + info;
                };
                return "API Key验证失败: " + hint;
            }
        } catch (Exception e) {
            return "API Key验证异常: " + e.getMessage();
        }
    }
}
