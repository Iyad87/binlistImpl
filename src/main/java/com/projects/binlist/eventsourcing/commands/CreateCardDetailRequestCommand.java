package com.projects.binlist.eventsourcing.commands;

import java.util.Date;

import org.springframework.http.HttpEntity;

public class CreateCardDetailRequestCommand extends BaseCommand<String> {

	private String cardNumber;
	
	private Date requestDate;
	
	private HttpEntity<?> entity;
	
	public CreateCardDetailRequestCommand(String id, String cardNumber, Date requestDate) {
		super(id);
		this.cardNumber = cardNumber;
		this.requestDate = requestDate;
	}

	public String getCardNumber() {
		return cardNumber;
	}

	public void setCardNumber(String cardNumber) {
		this.cardNumber = cardNumber;
	}

	public Date getRequestDate() {
		return requestDate;
	}

	public void setRequestDate(Date requestDate) {
		this.requestDate = requestDate;
	}

	public HttpEntity<?> getEntity() {
		return entity;
	}

	public void setEntity(HttpEntity<?> entity) {
		this.entity = entity;
	}

	
}
