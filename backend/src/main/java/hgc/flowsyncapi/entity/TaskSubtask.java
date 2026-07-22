package hgc.flowsyncapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("task_subtask")
public class TaskSubtask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String title;
    private Boolean completed;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
