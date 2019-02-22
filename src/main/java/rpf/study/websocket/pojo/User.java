package rpf.study.websocket.pojo;

import java.util.Objects;

public class User {
    private String name;
    private String channelId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public User(String name, String channelId) {
        this.name = name;
        this.channelId = channelId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(channelId, user.channelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId);
    }

    public User() {
    }

    @Override
    public String toString() {
        return "{" +
                "name:'" + name + '\'' +
                ", channelId:'" + channelId + '\'' +
                '}';
    }
}
