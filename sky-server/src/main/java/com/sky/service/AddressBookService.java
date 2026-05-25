package com.sky.service;

import com.sky.entity.AddressBook;

import java.util.List;

public interface AddressBookService {

    void save(AddressBook addressBook);

    List<AddressBook> list();

    AddressBook defaultList();

    void update(AddressBook addressBook);

    AddressBook getById(Long id);

    void delete(Long id);

    void setDefault(AddressBook addressBook);
}
