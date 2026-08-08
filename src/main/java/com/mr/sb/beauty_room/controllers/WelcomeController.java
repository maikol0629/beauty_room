package com.mr.sb.beauty_room.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
public class WelcomeController {


 @GetMapping("/")
    public ResponseEntity<?> welcome() {
        return ResponseEntity.ok("WELCOME TO BEAUTY ROOM API");
    }


}
