package info.mikaelsvensson.babyname.service.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdUtilsTest {

    @Test
    void randomAlphabet() {
        assertThat(IdUtils.randomAlphabet(123L)).isEqualTo("duhjkcpzfyxbqwgtnrsovlmiae");
        assertThat(IdUtils.randomAlphabet(123L)).isEqualTo("duhjkcpzfyxbqwgtnrsovlmiae");
        assertThat(IdUtils.randomAlphabet(123L)).isEqualTo("duhjkcpzfyxbqwgtnrsovlmiae");

        assertThat(IdUtils.randomAlphabet(456L)).isEqualTo("oinwrzaqkpmdjxevutbhsglyfc");
        assertThat(IdUtils.randomAlphabet(456L)).isEqualTo("oinwrzaqkpmdjxevutbhsglyfc");
        assertThat(IdUtils.randomAlphabet(456L)).isEqualTo("oinwrzaqkpmdjxevutbhsglyfc");

        assertThat(IdUtils.randomAlphabet(789L)).isEqualTo("pagtmyerohbznfuxdjsvicqwkl");
        assertThat(IdUtils.randomAlphabet(789L)).isEqualTo("pagtmyerohbznfuxdjsvicqwkl");
        assertThat(IdUtils.randomAlphabet(789L)).isEqualTo("pagtmyerohbznfuxdjsvicqwkl");
    }
}