package com.mrose.mint;

import com.google.common.base.MoreObjects;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;

/**
 * TODO(martinrose) : Add Documentation
 */
public class MintRow {
  private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy");
  private final DateTime date;
  private final String description;
  private final String originalDescription;
  private final BigDecimal amount;
  private final String transactionType;
  private final String accountName;
  private final String labels;
  private final String notes;

  private final String category;

  MintRow(String[] values) {
    int index = 0;
    date = dtf.parseDateTime(values[index++]);
    description = values[index++];
    originalDescription = values[index++];
    amount = new BigDecimal(values[index++]);
    transactionType = values[index++];
    String ignore1 = values[index++];
    accountName = cleanup(values[index++]);
    labels = values[index++];
    notes = values[index++];

    category = cleanup(StringUtils.substringAfterLast(getDescription(), "-"));
  }

  private static String cleanup(String s) {
    return StringUtils.upperCase(StringUtils.trimToEmpty(s)).replaceAll("\\s+", "");
  }

  public boolean isDebit() {
    return "debit".equals(transactionType);
  }

  public boolean isCredit() {
    return "credit".equals(transactionType);
  }

  public DateTime getDate() {
    return date;
  }

  public String getDescription() {
    return description;
  }

  public String getOriginalDescription() {
    return originalDescription;
  }

  public BigDecimal getFinancialAmount() {
    return (isDebit()) ? amount.negate() : amount;
  }

  public String getTransactionType() {
    return transactionType;
  }

  public String getCategory() {
    return category;
  }

  public String getAccountName() {
    return accountName;
  }

  public String getLabels() {
    return labels;
  }

  public String getNotes() {
    return notes;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("date", dtf.print(date))
        .add("description", description)
        .add("amount", getFinancialAmount())
        .add("ttype", transactionType)
        .add("account", accountName)
        .toString();
  }
}
