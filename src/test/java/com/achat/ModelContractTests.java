package com.achat;

import com.achat.augment.AugmentContext;
import com.achat.history.GameHistory;
import com.achat.home.Home;
import com.achat.round.RoundScore;
import com.achat.shop.EquipSlot;
import com.achat.shop.UserEquipment;
import com.achat.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelContractTests {
    private final ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void booleanPropertiesKeepTheirExistingJsonNames() {
        User user = new User("contract@example.com", "password-hash");
        user.setAdmin(true);
        user.verifyEmail();

        JsonNode userJson = mapper.valueToTree(user);
        assertThat(userJson.path("admin").asBoolean()).isTrue();
        assertThat(userJson.path("emailVerified").asBoolean()).isTrue();
        assertThat(userJson.has("isAdmin")).isFalse();
        assertThat(userJson.has("usernameKey")).isFalse();

        GameHistory history = new GameHistory(user.getId(), "room", "Room", true,
                4, 1, 100, 10, 20, 100, 50, null);
        JsonNode historyJson = mapper.valueToTree(history);
        assertThat(historyJson.path("matchRoom").asBoolean()).isTrue();
        assertThat(historyJson.has("isMatchRoom")).isFalse();
    }

    @Test
    void compatibilityAliasesKeepTheirExistingJsonProperties() {
        AugmentContext context = new AugmentContext("user", "room", "message");
        JsonNode contextJson = mapper.valueToTree(context);
        assertThat(contextJson.path("userId").asText()).isEqualTo("user");
        assertThat(contextJson.has("odLRXnwB")).isFalse();

        RoundScore score = new RoundScore("user");
        score.addScoredMessage();
        JsonNode scoreJson = mapper.valueToTree(score);
        assertThat(scoreJson.path("messageCount").asInt()).isEqualTo(1);
        assertThat(scoreJson.path("scoredMessageCount").asInt()).isEqualTo(1);
    }

    @Test
    void returnedCollectionsCannotOverwritePlacementOrEquipmentState() {
        User user = new User("copy@example.com", "password-hash");
        user.recordPlacementGame(2);
        user.getPlacementRanks().clear();
        assertThat(user.getPlacementRanks()).containsExactly(2);
        assertThat(user.getPlacementGamesPlayed()).isEqualTo(1);

        UserEquipment equipment = new UserEquipment(user.getId());
        equipment.equip(EquipSlot.TITLE, "title_newbie");
        equipment.getAllEquipped().clear();
        assertThat(equipment.getEquipped(EquipSlot.TITLE)).isEqualTo("title_newbie");
    }

    @Test
    void furnitureCoordinatesStayBoundedDuringJsonDeserialization() throws Exception {
        Home.FurniturePosition position = mapper.readValue("{\"x\":-20,\"y\":120}", Home.FurniturePosition.class);
        assertThat(position.getX()).isZero();
        assertThat(position.getY()).isEqualTo(100);

        Home.FurniturePosition created = new Home.FurniturePosition(120, -20);
        assertThat(created.getX()).isEqualTo(100);
        assertThat(created.getY()).isZero();
    }
}
