export interface RegisterStudentRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  password: string;
  matricule?: string;
  fieldOfStudy?: string;
  yearOfStudy?: number;
}

export interface RegisterLandlordRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  password: string;
  idCardNumber?: string;
  taxId?: string;
}

export interface LoginRequest {
  email?: string;
  phone?: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface UserProfile {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  profilePhotoUrl?: string;
  role: 'STUDENT' | 'LANDLORD' | 'ADMIN';
  registrationDate: string;
  matricule?: string;
  fieldOfStudy?: string;
  yearOfStudy?: number;
  verified?: boolean;
}
