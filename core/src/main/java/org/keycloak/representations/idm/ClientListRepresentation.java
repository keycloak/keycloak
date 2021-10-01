package org.keycloak.representations.idm;

import java.util.stream.Stream;

public class ClientListRepresentation {
	protected int first;
	protected int Max;
	protected long Total;
	protected Stream<ClientRepresentation> items;

	public Stream<ClientRepresentation> getItems() {
		return items;
	}

	public void setItems(Stream<ClientRepresentation> items) {
		this.items = items;
	}

	public Integer getFirst() {
		return first;
	}

	public void setFirst(int first) {
		this.first = first;
	}

	public Integer getMax() {
		return Max;
	}

	public void setMax(int max) {
		Max = max;
	}

	public long getTotal() {
		return Total;
	}

	public void setTotal(long total) {
		Total = total;
	}
}
