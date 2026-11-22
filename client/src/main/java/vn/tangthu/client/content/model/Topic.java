package vn.tangthu.client.content.model;

import java.util.UUID;

public record Topic(UUID id, String name, int sortOrder, long questionCount) {
    @Override
    public String toString() {
        return "#" + sortOrder + " · " + name + " (" + questionCount + ")";
    }
}
