package com.youthcompanion.controller;

import com.youthcompanion.model.InterestItem;
import com.youthcompanion.service.InterestService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@RestController
@RequestMapping("/api/interests")
public class InterestController {

    private final InterestService interestService;


    public InterestController(
            InterestService interestService
    ) {

        this.interestService =
                interestService;
    }


    /*
     * 获取全部兴趣
     *
     * GET /api/interests
     */
    @GetMapping
    public List<InterestItem> getAllInterests() {

        return interestService
                .getAllInterests();
    }


    /*
     * 随机推荐
     *
     * GET /api/interests/random?count=6
     */
    @GetMapping("/random")
    public List<InterestItem> getRandomInterests(

            @RequestParam(
                    defaultValue = "6"
            )
            int count
    ) {

        if (
                count < 1
                        ||
                        count > 20
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "count 必须是 1 到 20"
            );
        }


        return interestService
                .getRandomInterests(
                        count
                );
    }


    /*
     * 查询某一个
     *
     * GET /api/interests/I01
     */
    @GetMapping("/{id}")
    public InterestItem getInterest(
            @PathVariable String id
    ) {

        InterestItem item =
                interestService
                        .getInterestById(
                                id
                        );


        if (item == null) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "没有找到这个兴趣"
            );
        }


        return item;
    }
}