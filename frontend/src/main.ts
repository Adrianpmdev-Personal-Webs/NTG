/**
 * main
 *
 * Entry point of the application. Bootstraps the root component with the application configuration.
 *
 * @author @adrianpmdev
 */

import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { appConfig } from './app/app.config';

bootstrapApplication(App, appConfig).catch(error => console.error(error));
