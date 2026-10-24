import 'package:shared_preferences/shared_preferences.dart';

class OnboardingStore {
  static const _completedKey = 'onboarding_completed';

  Future<bool> isCompleted() async {
    try {
      return (await SharedPreferences.getInstance()).getBool(_completedKey) ?? false;
    } catch (_) {
      return false;
    }
  }

  Future<void> complete() async {
    try {
      await (await SharedPreferences.getInstance()).setBool(_completedKey, true);
    } catch (_) {
      // Onboarding remains usable for the current session if storage is unavailable.
    }
  }
}
