package com.antgroup.openspg.server.common.service.account.impl;

import com.antgroup.openspg.server.api.facade.Paged;
import com.antgroup.openspg.server.api.http.client.account.AccountService;
import com.antgroup.openspg.server.common.model.account.Account;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "env", havingValue = "default")
public class AccountServiceDefaultImpl implements AccountService {

  @Override
  public Account getLoginUser() {
    return null;
  }

  @Override
  public List<Account> getAccountByKeyword(String keyword) {
    return null;
  }

  @Override
  public Account getByUserNo(String userNo) {
    return null;
  }

  @Override
  public Account getWithPrivateByUserNo(String userNo) {
    return null;
  }

  @Override
  public Integer create(Account account) {
    return null;
  }

  @Override
  public Integer updatePassword(Account account) {
    return null;
  }

  @Override
  public Integer deleteAccount(String workNo) {
    return null;
  }

  @Override
  public Paged<Account> getAccountList(String account, Integer page, Integer size) {
    return null;
  }

  @Override
  public String getSha256HexPassword(String password, String salt) {
    return null;
  }

  @Override
  public Account getCurrentAccount(Cookie[] cookies) throws IOException {
    return null;
  }

  @Override
  public boolean login(Account account, HttpServletResponse response) {
    return false;
  }

  @Override
  public String logout(String workNo, String redirectUrl) {
    return null;
  }

  @Override
  public int updateUserConfig(Account account, Cookie[] cookies) {
    return 0;
  }
}
