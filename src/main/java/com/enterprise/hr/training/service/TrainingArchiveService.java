package com.enterprise.hr.training.service;

import com.enterprise.hr.training.dto.MyTrainingVO;

/**
 * 我的培训档案业务接口（F-07-06 员工自助查看）。
 *
 * @author Enterprise HR Team
 */
public interface TrainingArchiveService {

    /** 当前登录员工的培训档案：培训记录 + 报名记录 + 累计学时 */
    MyTrainingVO myTraining();
}
