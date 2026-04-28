package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CategoryMapper {
    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into category (type, name, sort, status, create_time, update_time, create_user, update_user) " +
            "VALUES ( " +
            "#{type},#{name},#{sort},#{status},#{createTime},#{updateTime},#{createUser},#{updateUser})")
    void save(Category category);

    Page<Category> page(CategoryPageQueryDTO categoryPageQueryDTO);
    @AutoFill(value = OperationType.UPDATE)
    void update(Category category);
//    @Select("SELECT * from sky_take_out.category where type=#{type}")
    Category list(Integer type);
    @Delete("DELETE from category where id=#{id}")
    void delete(Long id);
}
