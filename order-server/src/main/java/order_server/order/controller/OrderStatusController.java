package order_server.order.controller;

import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ConcurrentHashMap;


import java.util.Map;

@RestController
@RequestMapping("/status")
public class OrderStatusController {

    private final Map<String, String> orderStatusMap = new ConcurrentHashMap<>();

    @GetMapping("/{orderId}")
    public String getStatus(@PathVariable String orderId) {
        return orderStatusMap.getOrDefault(orderId, "NOT_FOUND");
    }

    @PostMapping("/{orderId}/{status}")
    public void updateStatus(@PathVariable String orderId, @PathVariable String status) {
        orderStatusMap.put(orderId, status);
    }
}
