package vn.tangthu.content.controller;

import java.util.Map;
import org.springframework.stereotype.Component;
import vn.tangthu.account.model.Role;
import vn.tangthu.common.socket.SocketController;
import vn.tangthu.content.dto.CreateTopic;
import vn.tangthu.content.dto.UpdateTopic;
import vn.tangthu.content.service.TopicService;

/**
 * M3 · Nội dung — chủ đề. Đọc cho UC05, quản lý cho UC22.
 *
 * <pre>
 * topic.list       —                       → [ {id, name, sortOrder, questionCount} ]  mọi vai
 * topic.questions  {topicId}               → [ {id, content} ]                         mọi vai
 * topic.create     {name, sortOrder?}      → topic                                     giáo vụ
 * topic.update     {id, name?, sortOrder?} → topic                                     giáo vụ
 * topic.delete     {id}                    → {}                                        giáo vụ
 * </pre>
 */
@Component
public class TopicSocketController implements SocketController {
    private final TopicService topics;

    public TopicSocketController(TopicService topics) {
        this.topics = topics;
    }

    @Override
    public void register(Routes routes) {
        routes.secured("topic.list", Role.USER, r -> topics.list());
        routes.secured("topic.questions", Role.USER, r -> topics.buttons(r.uuid("topicId")));
        routes.secured("topic.create", Role.CONTENT_ADMIN, r -> {
            CreateTopic req = r.body(CreateTopic.class);
            return topics.create(req.name(), req.sortOrder());
        });
        routes.secured("topic.update", Role.CONTENT_ADMIN, r -> {
            UpdateTopic req = r.body(UpdateTopic.class);
            return topics.update(r.uuid("id"), req.name(), req.sortOrder());
        });
        routes.secured("topic.delete", Role.CONTENT_ADMIN, r -> {
            topics.delete(r.uuid("id"));
            return Map.of();
        });
    }
}
