package com.youthcompanion.service;

import com.youthcompanion.model.Action;
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
public class ActionService {

    private final List<Action> actions;

    public ActionService(JsonMapper jsonMapper) throws IOException {

        ClassPathResource resource =
                new ClassPathResource("data/actions.json");

        try (InputStream inputStream = resource.getInputStream()) {

            actions = jsonMapper.readValue(
                    inputStream,
                    new TypeReference<List<Action>>() {}
            );
        }
    }


    public List<Action> getRandomActions(int level, int count) {

        List<Action> levelActions = actions.stream()
                .filter(action -> action.getLevel() == level)
                .toList();

        List<Action> shuffled =
                new ArrayList<>(levelActions);

        Collections.shuffle(shuffled);

        int resultCount =
                Math.min(count, shuffled.size());

        return shuffled.subList(0, resultCount);
    }
}