package com.youthcompanion.service;

import com.youthcompanion.model.InterestItem;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Service
public class InterestService {

    private final List<InterestItem> interests;


    public InterestService(
            JsonMapper jsonMapper
    ) throws IOException {

        ClassPathResource resource =
                new ClassPathResource(
                        "data/interests.json"
                );


        try (
                InputStream inputStream =
                        resource.getInputStream()
        ) {

            interests =
                    jsonMapper.readValue(
                            inputStream,
                            new TypeReference<
                                    List<InterestItem>
                                    >() {
                            }
                    );
        }
    }


    /*
     * 返回全部兴趣
     */
    public List<InterestItem> getAllInterests() {

        return interests;
    }


    /*
     * 根据 ID 找某个兴趣
     */
    public InterestItem getInterestById(
            String id
    ) {

        return interests
                .stream()
                .filter(
                        item ->
                                item
                                        .getId()
                                        .equals(id)
                )
                .findFirst()
                .orElse(null);
    }


    /*
     * 随机推荐几个方向
     */
    public List<InterestItem> getRandomInterests(
            int count
    ) {

        List<InterestItem> copy =
                new ArrayList<>(
                        interests
                );


        Collections.shuffle(copy);


        int resultCount =
                Math.min(
                        count,
                        copy.size()
                );


        return copy.subList(
                0,
                resultCount
        );
    }
}