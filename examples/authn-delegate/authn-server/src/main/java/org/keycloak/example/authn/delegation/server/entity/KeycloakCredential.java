package org.keycloak.example.authn.delegation.server.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
public class KeycloakCredential implements Serializable {

	private static final long serialVersionUID = 1L;
	
	//@Id
	//@GeneratedValue(strategy = GenerationType.IDENTITY)
	//private Integer id;

	@Id
	private String mark;
	
	private String code;
	
	private String execution;

	public String getMark() {
		return mark;
	}
	
	public void setMark(String mark) {
		this.mark = mark;
	}

	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public String getExecution() {
		return execution;
	}
	
	public void setExecution(String execution) {
		this.execution = execution;
	}
}