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
        String agentChallengePatch = Files.readString(Path.of("..", "sql", "patch_agent_challenge.sql"));

        assertThat(lastLoginPatch).contains("information_schema.COLUMNS");
        assertThat(lastLoginPatch).contains("COLUMN_NAME = 'last_login_time'");
        assertThat(scenarioPatch).contains("information_schema.COLUMNS");
        assertThat(scenarioPatch).contains("COLUMN_NAME = 'final_score'");
        assertThat(chatAgentPatch).contains("information_schema.COLUMNS");
        assertThat(chatAgentPatch).contains("COLUMN_NAME = 'answer_type'");
        assertThat(chatAgentPatch).contains("COLUMN_NAME = 'sources_json'");
        assertThat(agentChallengePatch).contains("agent_scenario");
        assertThat(agentChallengePatch).contains("COLUMN_NAME = 'agent_config'");
        assertThat(agentChallengePatch).contains("CREATE TABLE IF NOT EXISTS `agent_challenge_session`");
        assertThat(agentChallengePatch).contains("CREATE TABLE IF NOT EXISTS `agent_challenge_daily_reward`");
    }

    @Test
    void challengeSeedProvidesAgentScenarioChallenges() throws Exception {
        String seedSql = Files.readString(Path.of("..", "sql", "seed_challenge.sql"));

        assertThat(seedSql).contains("'agent_scenario'");
        assertThat(seedSql).contains("Agent模拟：刷单返利陷阱");
        assertThat(seedSql).contains("Agent模拟：冒充客服退款");
        assertThat(seedSql).contains("Agent模拟：虚假兼职招聘");
        assertThat(seedSql).contains("Agent模拟：校园贷征信恐吓");
        assertThat(seedSql).contains("Agent模拟：游戏账号交易");
        assertThat(seedSql).contains("Agent模拟：冒充公检法");
        assertThat(seedSql).contains("Agent模拟：贷款保证金");
        assertThat(seedSql).contains("Agent模拟：虚假投资理财");
    }
}
