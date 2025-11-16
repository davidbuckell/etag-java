package com.example.demo.controller;

import com.example.demo.service.Item;
import com.example.demo.service.ItemsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/items")
public class ItemsController {

    private final ItemsService itemsService;

    public ItemsController(ItemsService itemsService) {
        this.itemsService = itemsService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getItem(
            @PathVariable("id") int id,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        Item item = itemsService.findOne(id);
        if (item == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String etag = itemsService.computeETag(item);

        // Conditional GET: return 304 if ETag matches
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        // Return item with ETag header + body
        HttpHeaders headers = new HttpHeaders();
        headers.set("ETag", etag);

        // Build response JSON object
        return ResponseEntity.ok()
                .headers(headers)
                .body(new ItemWithEtag(item, etag));
    }

    // DTO wrapper for response including ETag in response body
    static class ItemWithEtag {
        public final int id;
        public final String name;
        public final double price;
        public final String updatedAt;
        public final String etag;

        public ItemWithEtag(Item item, String etag) {
            this.id = item.getId();
            this.name = item.getName();
            this.price = item.getPrice();
            this.updatedAt = item.getUpdatedAt();
            this.etag = etag;
        }
    }
}