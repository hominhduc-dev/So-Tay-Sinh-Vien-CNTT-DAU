package vn.tangthu.client.account.model;

/** Vai người dùng, xếp theo cấp: USER &lt; CONTENT_ADMIN &lt; SYSTEM_ADMIN. */
public enum Role {
    USER("Sinh viên"),
    CONTENT_ADMIN("Giáo vụ khoa"),
    SYSTEM_ADMIN("Quản trị viên");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public boolean atLeast(Role other) {
        return ordinal() >= other.ordinal();
    }

    @Override
    public String toString() {
        return label;
    }
}
