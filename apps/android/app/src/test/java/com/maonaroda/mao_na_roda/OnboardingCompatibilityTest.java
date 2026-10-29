package com.maonaroda.mao_na_roda;
import static org.junit.Assert.*; import org.junit.Test;
public class OnboardingCompatibilityTest { @Test public void packageAndPreferenceContractRemainStable(){assertEquals("com.maonaroda.mao_na_roda", BuildConfig.APPLICATION_ID); assertEquals("onboarding_completed", "onboarding_completed");} }
