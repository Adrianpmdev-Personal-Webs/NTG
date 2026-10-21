/**
 * API_BASE_URL
 *
 * Base URL of the backend API, taken from the build-time environment file.
 *
 * @author @adrianpmdev
 */

import { environment } from '../../../environments/environment';

export const API_BASE_URL = environment.apiBaseUrl;
