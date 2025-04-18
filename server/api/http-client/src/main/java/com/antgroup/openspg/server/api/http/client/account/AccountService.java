/*
 * Copyright 2023 OpenSPG Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 */

package com.antgroup.openspg.server.api.http.client.account;

import com.antgroup.openspg.server.api.facade.Paged;
import com.antgroup.openspg.server.common.model.account.Account;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/** get account from hr interface(ant) */
public interface AccountService {

  /**
   * get a login user from buc
   *
   * @return
   */
  Account getLoginUser();

  /**
   * get accounts by part of account info
   *
   * @param keyword
   * @return
   */
  List<Account> getAccountByKeyword(String keyword);

  /**
   * get account info by userNo
   *
   * @param userNo
   * @return
   */
  Account getByUserNo(String userNo);

  /**
   * get account info by userNo with private info
   *
   * @param userNo
   * @return
   */
  Account getWithPrivateByUserNo(String userNo);

  /**
   * create a new account
   *
   * @param account
   * @return
   */
  Integer create(Account account);

  /**
   * update password
   *
   * @param account
   * @return
   */
  Integer updatePassword(Account account);

  /**
   * delete account
   *
   * @param workNo
   * @return
   */
  Integer deleteAccount(String workNo);

  /**
   * get account list
   *
   * @param account
   * @param page
   * @param size
   * @return
   */
  Paged<Account> getAccountList(String account, Integer page, Integer size);

  /**
   * get sha256Hex password
   *
   * @param password
   * @param salt
   * @return
   */
  String getSha256HexPassword(String password, String salt);

  Account getCurrentAccount(Cookie[] cookies) throws IOException;

  boolean login(Account account, HttpServletResponse response);

  String logout(String workNo, String redirectUrl);

  /**
   * update user config
   *
   * @param account
   * @param cookies
   * @return
   */
  int updateUserConfig(Account account, Cookie[] cookies);
}
