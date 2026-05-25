package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AddressBookServiceImpl implements AddressBookService {

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Override
    public void save(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(StatusConstant.DISABLE);
        log.info("保存地址信息：{}", addressBook);
        addressBookMapper.insert(addressBook);
    }

    @Override
    public List<AddressBook> list() {
        Long userId = BaseContext.getCurrentId();
        log.info("查询当前用户地址信息{}", userId);
        AddressBook addressBook = new AddressBook();
        addressBook.setUserId(userId);
        return addressBookMapper.list(addressBook);
    }

    @Override
    public AddressBook defaultList() {
        AddressBook addressBook = new AddressBook();
        addressBook.setIsDefault(StatusConstant.ENABLE);
        List<AddressBook> list = addressBookMapper.list(addressBook);
        if (list == null || list.isEmpty()) {
            log.warn("当前用户未设置默认地址");
            return null;
        }
        AddressBook book = list.get(0);
        log.info("查询结果：{}", book);
        return book;
    }

    @Override
    public void update(AddressBook addressBook) {
        log.info("更新地址信息：{}", addressBook);
        addressBookMapper.update(addressBook);
    }

    @Override
    public AddressBook getById(Long id) {
        AddressBook addressBook = new AddressBook();
        addressBook.setId(id);
        List<AddressBook> list = addressBookMapper.list(addressBook);
        if (list == null || list.isEmpty()) {
            log.warn("用户地址不存在");
            return null;
        }
        AddressBook book = list.get(0);
        log.info("查询结果：{}", book);
        return book;
    }

    @Override
    public void delete(Long id) {
        log.info("删除地址信息：{}", id);
        addressBookMapper.delete(id);
    }

    @Override
    public void setDefault(AddressBook addressBook) {
        log.info("设置默认地址：{}", addressBook);
        // 将当前用户所有地址的默认地址取消
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(StatusConstant.DISABLE);
        addressBookMapper.updateByUserId(addressBook);
        // 设置当前地址为默认地址
        addressBook.setIsDefault(StatusConstant.ENABLE);
        addressBookMapper.update(addressBook);
    }
}
