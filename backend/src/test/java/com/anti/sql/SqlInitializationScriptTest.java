package com.anti.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SqlInitializationScriptTest {

    @Test
    void initScriptProvidesAdminAndStudentTrialAccounts() throws Exception {
        String initSql = Files.readString(Path.of("..", "sql", "init.sql"));

        assertThat(initSql).contains("'admin'");
        assertThat(initSql).contains("'student001'");
        assertThat(initSql).contains("'student'");
        assertThat(initSql).contains("'20260001'");
    }

    @Test
    void initScriptInitializesStudentScoreAndProfile() throws Exception {
        String initSql = Files.readString(Path.of("..", "sql", "init.sql"));

        assertThat(initSql).contains("INSERT INTO `user_score`");
        assertThat(initSql).contains("INSERT INTO `user_profile`");
        assertThat(initSql).contains("WHERE `username` = 'student001'");
        assertThat(initSql).contains("'newbie'");
    }

    @Test
    void patchScriptsAreSafeToRunAfterInitScript() throws Exception {
        String lastLoginPatch = Files.readString(Path.of("..", "sql", "patch_user_last_login.sql"));
        String scenarioPatch = Files.readString(Path.of("..", "sql", "patch_scenario_progress.sql"));
        String chatAgentPatch = Files.readString(Path.of("..", "sql", "patch_chat_agent.sql"));

        assertThat(lastLoginPatch).contains("information_schema.COLUMNS");
        assertThat(lastLoginPatch).contains("COLUMN_NAME = 'last_login_time'");
        assertThat(scenarioPatch).contains("information_schema.COLUMNS");
        assertThat(scenarioPatch).contains("COLUMN_NAME = 'final_score'");
        assertThat(chatAgentPatch).contains("information_schema.COLUMNS");
        assertThat(chatAgentPatch).contains("COLUMN_NAME = 'answer_type'");
        assertThat(chatAgentPatch).contains("COLUMN_NAME = 'sources_json'");
    }
}
