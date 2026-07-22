package hgc.flowsyncapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("task_comment")
public class TaskComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long userId;
    private String content;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
