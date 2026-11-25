// Tipos de la API de TriviaQuick. Reflejan los DTO del backend; el cliente nunca calcula puntuaciones.

export type Role = 'TEACHER' | 'STUDENT';
export type SessionStatus = 'LOBBY' | 'QUESTION' | 'EVENT' | 'FINISHED';
export type ViewerRole = 'HOST' | 'PARTICIPANT';
export type GameEventType = 'LIGHTNING_CHALLENGE' | 'MULTIPLIER' | 'DUEL' | 'SHIELD';
export type PowerUpType = 'MULTIPLIER' | 'SHIELD';
export type PowerUpStatus = 'AVAILABLE' | 'ACTIVE' | 'CONSUMED';

export interface User {
  id: number;
  email: string;
  displayName: string;
  role: Role;
}

export interface ApiErrorBody {
  status: number;
  code: string;
  message: string;
  fieldErrors?: Record<string, string>;
}

export interface TriviaSummary {
  id: number;
  title: string;
  description: string | null;
  questionCount: number;
  createdAt: string;
}

export interface SessionSummary {
  id: number;
  triviaTitle: string;
  status: SessionStatus;
  role: ViewerRole;
  joinCode: string | null;
  participantCount: number;
  createdAt: string;
  finishedAt: string | null;
}

export interface QuestionView {
  id: number;
  index: number;
  total: number;
  text: string;
  options: { id: number; text: string }[];
  timeLimitSeconds: number;
  basePoints: number;
  startedAt: string;
  deadline: string;
  type: QuestionType;
  imageUrl: string | null;
}

export interface MyAnswer {
  answerId: number;
  correct: boolean;
  pointsAwarded: number;
  multiplierApplied: boolean;
  speedBonus: number;
  multiplierFactor: number;
}

export interface PowerUp {
  id: number;
  type: PowerUpType;
  status: PowerUpStatus;
  grantedAt: string;
}

export interface GameEvent {
  id: number;
  type: GameEventType;
  status: 'ACTIVE' | 'RESOLVED';
  afterQuestionIndex: number;
  startedAt: string;
  expiresAt: string;
  challenge: { category: string; prompt: string; options: string[] } | null;
  eligible: boolean;
  responded: boolean;
  myResponseCorrect: boolean | null;
  duelists: string[];
  responsesCount: number;
  outcome: string | null;
}

export interface MyStanding {
  participantId: number;
  displayName: string;
  score: number;
  correctAnswers: number;
  answeredCount: number;
  position: number;
  currentStreak: number;
  bestStreak: number;
}

export interface ParticipantBrief {
  participantId: number;
  displayName: string;
  initials: string;
}

export interface SessionState {
  id: number;
  triviaTitle: string;
  status: SessionStatus;
  role: ViewerRole;
  joinCode: string | null;
  serverTime: string;
  questionCount: number;
  currentQuestion: QuestionView | null;
  myAnswer: MyAnswer | null;
  answersReceived: number | null;
  currentEvent: GameEvent | null;
  me: MyStanding | null;
  myPowerUps: PowerUp[];
  participants: ParticipantBrief[];
  lastQuestion: boolean;
}

export interface AnswerResult {
  correct: boolean;
  pointsAwarded: number;
  multiplierApplied: boolean;
  totalScore: number;
  correctAnswerId: number | null;
  basePoints: number;
  speedBonus: number;
  multiplierFactor: number;
  streak: number;
  streakBonus: number;
}

export interface EventResponseResult {
  correct: boolean;
  pointsAwarded: number;
  totalScore: number;
  message: string;
}

export interface ParticipantRanking {
  participantId: number;
  position: number;
  displayName: string;
  initials: string;
  score: number;
  correctAnswers: number;
  currentUser: boolean;
  tied: boolean;
}

export interface Ranking {
  sessionId: number;
  status: SessionStatus;
  provisional: boolean;
  tieBreakRule: string;
  entries: ParticipantRanking[];
}

export interface PodiumPlace {
  position: number;
  score: number;
  participants: ParticipantRanking[];
}

export interface Podium {
  sessionId: number;
  places: PodiumPlace[];
}

export interface GameResult {
  sessionId: number;
  triviaTitle: string;
  finishedAt: string;
  participantCount: number;
  questionCount: number;
  myResult: ParticipantRanking | null;
  podium: Podium;
  ranking: ParticipantRanking[];
  tieBreakRule: string;
}

// ------------------------------------------------------------------ v2: editor, puntuación y perfil

export type TriviaStatus = 'DRAFT' | 'PUBLISHED';
export type PlayMode = 'INDIVIDUAL' | 'GROUP';
export type EventMode = 'AUTO' | 'MANUAL' | 'OFF';
export type QuestionType = 'MULTIPLE_CHOICE' | 'TRUE_FALSE';
export type ChallengeCategory = 'ANY' | 'LOGIC' | 'MATH' | 'MEMORY' | 'KNOWLEDGE';
export type PowerUpTarget = 'BOTTOM_HALF' | 'ALL' | 'CORRECT_PREVIOUS';

export interface AnswerDraft {
  id?: number;
  text: string;
  correct: boolean;
}

export interface QuestionDraft {
  id?: number;
  type: QuestionType;
  text: string;
  timeLimitSeconds: number;
  basePoints: number;
  imageKey: string | null;
  imageUrl?: string | null;
  answers: AnswerDraft[];
}

export interface SpeedTier {
  maxElapsedPercent: number;
  bonusPoints: number;
}

export interface StreakTier {
  streakLength: number;
  bonusPoints: number;
}

export interface ScoringSettings {
  defaultPoints: number;
  speedBonusEnabled: boolean;
  speedTiers: SpeedTier[];
  streakEnabled: boolean;
  streakBreaksOnMiss: boolean;
  streakTiers: StreakTier[];
}

export interface AbilitySettings {
  lightningSeconds: number;
  lightningReward: number;
  lightningCategory: ChallengeCategory;
  multiplierFactor: number;
  multiplierTarget: PowerUpTarget;
  shieldTarget: PowerUpTarget;
  duelSeconds: number;
  duelStake: number;
  powerUpSeconds: number;
}

export interface EventSettings {
  mode: EventMode;
  autoFrequency: number;
  autoTypes: GameEventType[];
  abilities: AbilitySettings;
}

export interface TimelineEventDraft {
  afterQuestion: number;
  type: GameEventType;
  durationSeconds: number | null;
  rewardPoints: number | null;
  multiplierFactor: number | null;
  stake: number | null;
  challengeCategory: ChallengeCategory | null;
  target: PowerUpTarget | null;
}

export interface TriviaDetail {
  id: number;
  title: string;
  description: string | null;
  category: string | null;
  status: TriviaStatus;
  playMode: PlayMode;
  coverImageKey: string | null;
  coverImageUrl: string | null;
  activityMinutes: number | null;
  createdAt: string;
  updatedAt: string;
  questionsLocked: boolean;
  inUse: boolean;
  questions: (QuestionDraft & { id: number; position: number })[];
  scoring: ScoringSettings;
  events: EventSettings;
  timeline: TimelineEventDraft[];
}

export interface TriviaUpsert {
  title: string;
  description: string | null;
  category: string | null;
  status: TriviaStatus;
  playMode: PlayMode;
  coverImageKey: string | null;
  activityMinutes: number | null;
  questions: QuestionDraft[];
  scoring: ScoringSettings;
  events: EventSettings;
  timeline: TimelineEventDraft[];
}

export interface TriviaListItem {
  id: number;
  title: string;
  description: string | null;
  category: string | null;
  status: TriviaStatus;
  playMode: PlayMode;
  questionCount: number;
  coverImageUrl: string | null;
  createdAt: string;
  updatedAt: string;
  sessionsPlayed: number;
  questionsLocked: boolean;
}

export interface TeacherDashboard {
  totalQuizzes: number;
  publishedQuizzes: number;
  draftQuizzes: number;
  finishedSessions: number;
  recentResults: {
    sessionId: number;
    triviaTitle: string;
    finishedAt: string;
    participantCount: number;
    winners: string[];
    topScore: number | null;
  }[];
}

export interface Preferences {
  soundEnabled: boolean;
  masterVolume: number;
  effectsVolume: number;
  countdownSounds: boolean;
}

export interface Profile {
  id: number;
  email: string;
  displayName: string;
  role: Role;
  avatarUrl: string | null;
  pendingEmail: string | null;
  createdAt: string;
  preferences: Preferences;
}
