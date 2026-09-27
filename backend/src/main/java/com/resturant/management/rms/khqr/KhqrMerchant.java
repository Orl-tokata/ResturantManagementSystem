package com.resturant.management.rms.khqr;

/**
 * Who is being paid.
 *
 * @param accountId     Bakong account, e.g. {@code somebody@aclb}. The QR is
 *                      only spendable if this is real — everything else here is
 *                      presentation.
 * @param name          shown in the payer's app before they confirm
 * @param city          required by the format; "Phnom Penh" if unset
 * @param acquiringBank optional, names the bank behind the account
 * @param storeLabel    optional, which branch
 * @param terminalLabel optional, which till
 */
public record KhqrMerchant(
        String accountId,
        String name,
        String city,
        String acquiringBank,
        String storeLabel,
        String terminalLabel) {
}
