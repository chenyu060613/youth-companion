package com.youthcompanion.controller;

import com.youthcompanion.model.Action;
import com.youthcompanion.service.ActionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/actions")
public class ActionController {

    private final ActionService actionService;

    public ActionController(ActionService actionService) {
        this.actionService = actionService;
    }


    @GetMapping
    public List<Action> getActions(
            @RequestParam int level,
            @RequestParam(defaultValue = "3") int count
    ) {

        if (level < 1 || level > 4) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "level 必须是 1 到 4"
            );
        }

        if (count < 1 || count > 10) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "count 必须是 1 到 10"
            );
        }

        return actionService.getRandomActions(level, count);
    }
}