package com.specknet.pdiotapp.bean;

public class UserInfoBean {

    private String email;
    private Integer age;
    private String gender;

    private String naem;


    public UserInfoBean() {
    }


    public UserInfoBean(String email, Integer age, String gender, String naem) {
        this.email = email;
        this.age = age;
        this.gender = gender;
        this.naem = naem;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getNaem() {
        return naem;
    }

    public void setNaem(String naem) {
        this.naem = naem;
    }
}
