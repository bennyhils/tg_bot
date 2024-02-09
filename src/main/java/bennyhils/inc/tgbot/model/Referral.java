package bennyhils.inc.tgbot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Referral {
    //кто пришел
    private long referralId;
    //кто пригласил пришедшего
    private long referrerId;
    private int firstPaymentMonthCount;
    private boolean isActivated;
    private Instant activatingTime;
}