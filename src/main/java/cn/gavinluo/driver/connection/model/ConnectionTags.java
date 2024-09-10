package cn.gavinluo.driver.connection.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConnectionTags implements Serializable {
    private static final long serialVersionUID = 588562385470368209L;

    @JsonProperty("connection")
    private Connection connection;
    @JsonProperty("tags")
    private List<Tag> tags;
}
