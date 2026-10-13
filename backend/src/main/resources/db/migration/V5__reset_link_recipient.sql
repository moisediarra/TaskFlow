-- A reset link only works while the account still has the address it was sent to, so a link that reached a
-- mistyped address stops working once the email is corrected, whatever the timing. Links sent before this
-- column existed have none and are refused; people simply request a new one.
ALTER TABLE password_reset_tokens ADD COLUMN sent_to varchar(254);
