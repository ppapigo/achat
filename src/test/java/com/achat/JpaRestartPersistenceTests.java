package com.achat;

import com.achat.auth.AuthService;
import com.achat.auth.dto.LoginRequest;
import com.achat.auth.dto.RegisterRequest;
import com.achat.board.BoardCategory;
import com.achat.board.BoardService;
import com.achat.board.dto.BoardCreateRequest;
import com.achat.season.SeasonStore;
import com.achat.shop.EquipSlot;
import com.achat.shop.ShopService;
import com.achat.user.User;
import com.achat.user.UserStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class JpaRestartPersistenceTests {
    @TempDir Path databaseDirectory;

    @Test
    void committedDataSurvivesApplicationRestart() {
        String userId;
        Long boardId;
        int seasonId;
        String messageId;
        try (var first = startApplication()) {
            userId = first.getBean(AuthService.class)
                    .register(new RegisterRequest("restart@example.com", "password123!")).userId();
            var shop = first.getBean(ShopService.class);
            shop.purchase(userId, "title_newbie");
            shop.equip(userId, "title_newbie");
            boardId = first.getBean(BoardService.class).createBoard(userId,
                    new BoardCreateRequest(BoardCategory.FREE, "persisted", "survives a restart")).id();
            seasonId = first.getBean(SeasonStore.class).getCurrentSeason().getId();
            var message = new com.achat.message.Message("restart-room", userId, null, "persisted dice", "persisted dice", "FATE_DICE");
            message.setAugmentEffects(java.util.List.of(new com.achat.augment.effect.MessageEffect(
                    com.achat.augment.AugmentType.FATE_DICE, "WON", null, null, 6, 10, null, 1, 10)));
            first.getBean(com.achat.message.MessageStore.class).save(message);
            messageId = message.getId();
        }
        try (var second = startApplication()) {
            User user = second.getBean(UserStore.class).findById(userId).orElseThrow();
            assertThat(second.getBean(AuthService.class)
                    .login(new LoginRequest("restart@example.com", "password123!")).userId()).isEqualTo(userId);
            assertThat(user.getCoins()).isEqualTo(900);
            var effect = second.getBean(com.achat.message.MessageStore.class).findById(messageId).orElseThrow().getAugmentEffects().getFirst();
            assertThat(effect.value()).isEqualTo(6);
            assertThat(effect.points()).isEqualTo(10);
            assertThat(second.getBean(UserStore.class).count()).isEqualTo(1);
            assertThat(second.getBean(ShopService.class).getEquipment(userId).getEquipped(EquipSlot.TITLE)).isEqualTo("title_newbie");
            assertThat(second.getBean(BoardService.class).getBoard(boardId, userId).title()).isEqualTo("persisted");
            assertThat(second.getBean(SeasonStore.class).getCurrentSeason().getId()).isEqualTo(seasonId);
            assertThat(second.getBean(BoardService.class).createBoard(userId,
                    new BoardCreateRequest(BoardCategory.FREE, "next", "next persisted board")).id()).isGreaterThan(boardId);
        }
    }

    private ConfigurableApplicationContext startApplication() {
        String url = "jdbc:h2:file:" + databaseDirectory.resolve("restart").toAbsolutePath().toString().replace('\\', '/')
                + ";MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE";
        return new SpringApplicationBuilder(AchatApplication.class).run(
                "--server.port=0", "--spring.datasource.url=" + url,
                "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.datasource.username=sa", "--spring.datasource.password=",
                "--spring.jpa.hibernate.ddl-auto=update");
    }
}
