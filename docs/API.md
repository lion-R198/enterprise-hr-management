# API 文档

Base URL：`http://localhost:8080/api`

所有响应统一使用 `R<T>`：

```json
{ "code": 200, "message": "success", "data": <T> }
```

| Method | Path                                  | 说明                          |
| ------ | ------------------------------------- | ----------------------------- |
| GET    | `/employees`                          | 全部员工列表                  |
| GET    | `/employees/{id}`                     | 按主键查询员工                |
| GET    | `/employees/by-emp-no/{empNo}`        | 按工号查询员工                |
| GET    | `/employees/page`                     | 分页 + 关键字（姓名/工号）模糊 |
| POST   | `/employees`                          | 新增员工                      |
| PUT    | `/employees/{id}`                     | 修改员工（按 id）             |
| DELETE | `/employees/{id}`                     | 按 id 删除（逻辑删除）        |
| DELETE | `/employees/batch`                    | 批量删除（逻辑删除）          |
| GET    | `/depts`                              | 全部部门列表                  |

## 错误响应

| HTTP | code | 含义               |
| ---- | ---- | ------------------ |
| 400  | 400  | 参数错误 / 校验失败 |
| 500  | 500  | 服务器内部异常     |

## 关键 SQL（由 MyBatis-Plus 自动生成）

- 列表：`SELECT id, emp_no, name, ... FROM employee WHERE deleted=0 ORDER BY id DESC`
- 按工号：`... WHERE emp_no = #{empNo} AND deleted=0`
- 分页：`... WHERE (name LIKE ? OR emp_no LIKE ?) AND deleted=0 ORDER BY id DESC LIMIT ?, ?`
