package org.dbdoclet.mimir.search;

public class SearchFieldItem {
	
	private String label;
	private String value;

	
	public SearchFieldItem(String label, String value) {
		super();
		this.label = label;
		this.value = value;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SearchFieldItem other = (SearchFieldItem) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	public String getLabel() {
		return label;
	}
	
	public String getValue() {
		return value;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return label;
	}
}
