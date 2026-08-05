package com.mr.sb.beauty_room.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
public class Welcome {


 @GetMapping("/")
    public ResponseEntity<?> findAll() {
        return ResponseEntity.ok("WELCOME TO BEAUTY ROOM API");
    }

    
}
